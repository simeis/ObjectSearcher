package com.simeiscomet.objectsearcher;

import android.graphics.PointF;

/**
 * Created by NKJ on 2016/09/01.
 */
public class Cluster {
    public Cluster( PointF point0, int cluster0 )
    {
        point = point0;
        cluster = cluster0;
    }

    public PointF point;
    public int   cluster;
}
