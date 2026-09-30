package com.marketintel.model;
import java.util.List;
public record WalkForwardResult(String symbol,String model,int totalTrainSamples,int totalTestSamples,double accuracy,double precision,double recall,double f1,List<WalkForwardFold> folds) {}