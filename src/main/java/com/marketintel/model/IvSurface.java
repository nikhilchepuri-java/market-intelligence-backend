package com.marketintel.model;

import java.util.List;

public record IvSurface(double spotPrice, List<IvSurfacePoint> points, List<IvSkew> skews) {}
