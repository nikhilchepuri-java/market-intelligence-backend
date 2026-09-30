package com.marketintel.model;
public record WalkForwardFold(int fold,int trainSamples,int testSamples,double accuracy,double precision,double recall,double f1) {}