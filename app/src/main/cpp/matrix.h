#ifndef __MATRIX_H__
#define __MATRIX_H__

#include <iostream>
#include <vector>


template <class T>
class Matrix {
    private:
        std::vector<std::vector<T> > data;

    public:
        Matrix();
        Matrix(int nbLine, int nbCol);

        int sizeLine() const;
        int sizeColumn() const;

        T get(int line, int col) const;
        Matrix<T> getLine(int line) const;
        Matrix<T> getColumn(int col) const;
        void set(int line, int col, T val);

        T trace();
        T sum();

        void draw();

        Matrix<T> operator+ (const Matrix<T> & B);
        Matrix<T> operator- (const Matrix<T> &B);
        Matrix<T> operator* (const T &val);
        Matrix<T> operator* (const Matrix<T> &B);

};

#endif
