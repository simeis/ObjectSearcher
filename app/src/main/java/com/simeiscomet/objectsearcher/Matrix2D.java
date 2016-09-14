package com.simeiscomet.objectsearcher;

/**
 * Created by emoson(http://emoson.hateblo.jp/entry/2015/10/30/052146) on 2015/10/30.
 */
public class Matrix2D {
    private double[][] matrix;

    public Matrix2D(double[] vector){
        this.matrix = new double[vector.length][1];
        for(int i=0; i<vector.length; i++){
            this.matrix[i][0] = vector[i];
        }
    }

    public Matrix2D(double[][] vector){
        this.matrix = new double[vector.length][vector[0].length];
        for(int r=0; r<vector.length; r++){
            for(int c=0; c<vector[r].length; c++){
                this.matrix[r][c] = vector[r][c];
            }
        }
    }

    //行列の取得
    public double[][] getArrays(){
        return this.matrix;
    }

    //行数を取得
    public int getRow(){
        return this.matrix.length;
    }

    //列数を取得
    public int getCol(){
        return this.matrix[0].length;
    }

    //転置行列を取得
    public Matrix2D T(){
        double[][] t = new double[this.getCol()][this.getRow()];
        for(int r=0; r<t.length; r++){
            for(int c=0; c<t[r].length; c++){
                t[r][c] = this.matrix[c][r];
            }
        }
        return new Matrix2D(t);
    }

    private void changeValue(int row, int col, double a){
        this.matrix[row][col] = a;
    }

    private double getValue(int row, int col){
        return this.matrix[row][col];
    }

    //行列の内積
    public static Matrix2D dot(Matrix2D a, Matrix2D b){
        double[][] d = new double[a.getRow()][b.getCol()];
        for(int r=0; r<a.getRow(); r++){
            for(int c=0; c<b.getCol(); c++){
                double sum = 0;
                for(int k=0; k<b.getRow(); k++){
                    sum += a.getValue(r, k) * b.getValue(k, c);
                }
                d[r][c] = sum;
            }
        }
        return new Matrix2D(d);
    }

    //行列の加算
    public static Matrix2D add(Matrix2D a, Matrix2D b){
        double[][] d = new double[a.getRow()][a.getCol()];
        for(int c=0; c<a.getRow(); c++){
            for(int r=0; r<a.getCol(); r++){
                d[r][c] = a.getValue(r, c) + b.getValue(r, c);
            }
        }
        return new Matrix2D(d);
    }

    //行列の減算
    public static Matrix2D sub(Matrix2D a, Matrix2D b){
        double[][] d = new double[a.getRow()][a.getCol()];
        for(int c=0; c<a.getRow(); c++){
            for(int r=0; r<a.getCol(); r++){
                d[r][c] = a.getValue(r, c) - b.getValue(r, c);
            }
        }
        return new Matrix2D(d);
    }

    //行列のルート計算
    public static Matrix2D sqrt(Matrix2D a){
        double[][] d = new double[a.getRow()][a.getCol()];
        for(int r=0; r<a.getRow(); r++){
            for(int c=0; c<a.getCol(); c++){
                d[r][c] = Math.sqrt(a.getValue(r, c));
            }
        }
        return new Matrix2D(d);
    }

    //行列のルート計算
    public static Matrix2D sqrt(Matrix2D a, double b){
        double[][] d = new double[a.getRow()][a.getCol()];
        for(int r=0; r<a.getRow(); r++){
            for(int c=0; c<a.getCol(); c++){
                d[r][c] = Math.pow(a.getValue(r, c), b);
            }
        }
        return new Matrix2D(d);
    }

    //行列の定数倍
    public static Matrix2D prod(double a, Matrix2D b){
        double[][] d = new double[b.getRow()][b.getCol()];
        for(int r=0; r<b.getRow(); r++){
            for(int c=0; c<b.getCol(); c++){
                d[r][c] = b.getValue(r, c) * a;
            }
        }
        return new Matrix2D(d);
    }

    //行列の積
    public static Matrix2D mult(Matrix2D a, Matrix2D b){
        if(a.getCol() == b.getRow()){
            double[][] d = new double[a.getRow()][b.getCol()];
            for(int r=0; r<a.getRow(); r++){
                for(int c=0; c<b.getCol(); c++){
                    double sum = 0;
                    for(int i=0; i<b.getRow(); i++){
                        sum += a.getValue(r, i) * b.getValue(i, c);
                    }
                    d[r][c] = sum;
                }
            }
            return new Matrix2D(d);
        }
        else{
            return null;
        }
    }

    //行列の横方向への結合
    public static Matrix2D catHorizon(Matrix2D a, Matrix2D b){
        if(a.getRow() == b.getRow()){
            double[][] d = new double[a.getRow()][a.getCol() + b.getCol()];
            for(int r=0; r<a.getRow(); r++){
                for(int c=0; c<a.getCol(); c++){
                    d[r][c] = a.getValue(r, c);
                }
                for(int c=0; c<b.getCol(); c++){
                    d[r][a.getCol()+c] = b.getValue(r, c);
                }
            }
            return new Matrix2D(d);
        }
        return null;
    }

    //行列の縦方向への結合
    public static Matrix2D catVertical(Matrix2D a, Matrix2D b){
        if(a.getCol() == b.getCol()){
            double[][] d = new double[a.getRow() + b.getRow()][a.getCol()];
            for(int c=0; c<a.getCol(); c++){
                for(int r=0; r<a.getRow(); r++){
                    d[r][c] = a.getValue(r, c);
                }
                for(int r=0; r<b.getRow(); r++){
                    d[a.getRow()+r][c] = b.getValue(r, c);
                }
            }
            return new Matrix2D(d);
        }
        else return null;
    }

    private static Matrix2D pivodRow(int i, int j, Matrix2D a){
        double[][] b = new double[a.getRow()][a.getCol()];
        for(int r=0; r<a.getRow(); r++){
            for(int c=0; c<a.getCol(); c++){
                if(r == i)b[r][c] = a.getValue(j, c);
                else if(r == j)b[r][c] = a.getValue(i, c);
                else b[r][c] = a.getValue(r, c);
            }
        }
        return new Matrix2D(b);
    }

    //逆行列の取得
    public static Matrix2D inv(Matrix2D a){
        Matrix2D d = Matrix2D.catHorizon(a, Matrix2D.IdentityMatrix(a.getRow()));
        /*for(int r=0; r<a.getRow(); r++){
            if(a.getValue(r, r) >= 0)continue;
            for(int i=r+1; i<a.getRow(); i++){
                if(a.getValue(i, r) >= 0){
                    d = Matrix2D.pivodRow(r, i, d);
                    break;
                }
            }
        }*/

        for(int r=0; r<a.getCol(); r++){
            double max = Math.abs(d.getValue(r,r));
            int index = r;
            for(int rr=r+1; rr<a.getCol(); rr++){
                if(Math.abs(d.getValue(rr,r))>max){
                    max = Math.abs(d.getValue(rr, r));
                    index = rr;
                }
            }
            d = Matrix2D.pivodRow(r, index, d);

            double A = 1.0 / d.getValue(r, r);
            for(int c=0; c<d.getCol(); c++){
                d.changeValue(r, c, A*d.getValue(r, c));
            }

            for(int i=r+1; i<a.getRow(); i++){
                double B = d.getValue(i, r);
                for(int c=0; c<d.getCol(); c++){
                    d.changeValue(i, c, d.getValue(i, c)-B*d.getValue(r, c));
                }
            }
        }

        for(int r=0; r<a.getRow(); r++){
            for(int i=r+1; i<a.getCol(); i++){
                double B = d.getValue(r, i);
                for(int j=i; j<d.getCol(); j++){
                    d.changeValue(r, j, d.getValue(r, j) - B*d.getValue(i, j));
                }
            }
        }
        double[][] e = new double[a.getRow()][a.getCol()];
        for(int r=0; r<a.getRow(); r++){
            for(int c=0; c<a.getCol(); c++){
                e[r][c] = d.getValue(r, c+a.getCol());
            }
        }
        return new Matrix2D(e);
    }

    //単位行列の取得
    public static Matrix2D IdentityMatrix(int N){
        double[][] a = new double[N][N];
        for(int r=0; r<N; r++){
            for(int c=0; c<N; c++){
                if(r == c)a[r][c] = 1.0;
            }
        }
        return new Matrix2D(a);
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        for(int r=0; r<getRow(); r++){
            sb.append("|");
            for(int c=0; c<getCol(); c++){
                if(matrix[r][c] < 0)sb.append(String.format("%.5f ", matrix[r][c]));
                else sb.append(String.format(" %.5f ", matrix[r][c]));
            }
            sb.append("|\n");
        }
        return sb.toString();
    }
}
