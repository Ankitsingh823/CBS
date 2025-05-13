package com.model;

public class ConfigurableBlock {

    private int maxBlocksAllowed;
    private int blockTimeperiod;
    private int maxTransactionsPerBlock;

    // Constructors
    public ConfigurableBlock() {}

    public ConfigurableBlock(int maxBlocksAllowed, int blockTimeperiod, int maxTransactionsPerBlock) {
        this.maxBlocksAllowed = maxBlocksAllowed;
        this.blockTimeperiod = blockTimeperiod;
        this.maxTransactionsPerBlock = maxTransactionsPerBlock;
    }

    // Getters and Setters
    public int getMaxBlocksAllowed() {
        return maxBlocksAllowed;
    }

    public void setMaxBlocksAllowed(int maxBlocksAllowed) {
        this.maxBlocksAllowed = maxBlocksAllowed;
    }

    public int getBlockTimeperiod() {
        return blockTimeperiod;
    }

    public void setBlockTimeperiod(int blockTimeperiod) {
        this.blockTimeperiod = blockTimeperiod;
    }

    public int getMaxTransactionsPerBlock() {
        return maxTransactionsPerBlock;
    }

    public void setMaxTransactionsPerBlock(int maxTransactionsPerBlock) {
        this.maxTransactionsPerBlock = maxTransactionsPerBlock;
    }

    @Override
    public String toString() {
        return "ConfigurableBlock{" +
                "maxBlocksAllowed=" + maxBlocksAllowed +
                ", blockTimeperiod=" + blockTimeperiod +
                ", maxTransactionsPerBlock=" + maxTransactionsPerBlock +
                '}';
    }
}
