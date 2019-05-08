public class TxHandler 
{

    UTXOPool unspendPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) 
    {
        unspendPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) 
    {
        if(tx == null) {return false;}

        int numTXInputs = tx.numInputs();
        int numTXOutputs = tx.numOutputs();

        UTXOPool tmpPool = new UTXOPool();

        double sumInput = 0;

        for(int i=0; i<numTXInputs; i++)
        {
            UTXO prevTX = new UTXO(tx.getInput(i).prevTxHash,tx.getInput(i).outputIndex);

            // (1) all outputs claimed by {@code tx} are in the current UTXO pool
            if(unspendPool.contains(prevTX) == false)
            {
                return false;
            }

            // (2) the signatures on each input of {@code tx} are valid
            if(Crypto.verifySignature(unspendPool.getTxOutput(prevTX).address, tx.getRawDataToSign(i), tx.getInput(i).signature) == false)
            {
                return false;
            }

            // (3) no UTXO is claimed multiple times by {@code tx}
            if(tmpPool.contains(prevTX))
            {
                return false;
            }
            else
            {
                tmpPool.addUTXO(prevTX, unspendPool.getTxOutput(prevTX));
            }

            // (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output values; and false otherwise.
            sumInput +=  unspendPool.getTxOutput(prevTX).value;
        }

        double sumOutput = 0;

        for(int i=0; i<numTXOutputs; i++)
        {
            // (4) all of {@code tx}s output values are non-negative
            if(tx.getOutput(i).value < 0)
            {
                return false;
            }

            // (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output values; and false otherwise.
            sumOutput += tx.getOutput(i).value;
        }

        // (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output values; and false otherwise.
        if(sumInput < sumOutput){return false;}

        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) 
    {
        Transaction[] validTXs = new Transaction[possibleTxs.length];
        int i = 0;

        UTXOPool tmpPool = new UTXOPool();

        /*for(int j=0; j<possibleTxs.length; j++) 
        {
            Transaction tx = possibleTxs[j];
        */
        for(Transaction tx : possibleTxs) 
        {
            
            if(isValidTx(tx) == false){continue;}

            int j=0;
            while(j<tx.numInputs())
            { 
                UTXO prevTX = new UTXO(tx.getInput(j).prevTxHash,tx.getInput(j).outputIndex);
                if(tmpPool.contains(prevTX)){break;}
                else{tmpPool.addUTXO(prevTX, unspendPool.getTxOutput(prevTX));}
                j++;
            }

            if(j != tx.numInputs()){continue;}

            validTXs[i] = tx;
            i++;
        }

        return validTXs;
    }
}
