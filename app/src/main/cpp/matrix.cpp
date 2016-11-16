#include <cassert>
#include <vector>

#include "matrix.h"



template <class T>
Matrix<T>::Matrix(){}


template <class T>
Matrix<T>::Matrix(int line, int column){
    for(int l = 0; l < line; ++l){
        std::vector<T> tmp;
        tmp.reserve(column);

        for(int c = 0; c < column; ++c){
            tmp.push_back(0);//valeur par defaut
        }

        data.push_back(tmp);
    }
}




template <class T>
int Matrix<T>::sizeLine() const{
    return data.size();
}

template <class T>
int Matrix<T>::sizeColumn() const{
    if (data.size() == 0)
        return 0;

    return data[0].size();
}

template <class T>
T Matrix<T>::get(int line, int col) const{
    assert(line < sizeLine());
    assert(col < sizeColumn());

    return data[line][col];
}

template <class T>
Matrix<T> Matrix<T>::getLine(int line) const {
    Matrix<T> output(1, this->sizeColumn());

    for (int c = 0; c < this->sizeColumn(); ++c)
        output.set(0, c, this->get(line, c));

    return output;
}

template <class T>
Matrix<T> Matrix<T>::getColumn(int col) const {
    Matrix<T> output(this->sizeLine(), 1);

    for (int l = 0; l < this->sizeLine(); ++l)
        output.set(l, 0, this->get(l, col));

    return output;
}


template <class T>
void Matrix<T>::set(int line, int col, T val) {
    assert(line < sizeLine());
    assert(col < sizeColumn());

    data[line][col] = val;
}


template <class T>
T Matrix<T>::trace() {
    T out = (T) 0;

    assert(sizeLine() == sizeColumn());

    for (int i = 0; i < sizeLine(); ++i)
        out += get(i, i);

    return out;
}

template <class T>
T Matrix<T>::sum() {
    T out = (T) 0;

    for (int i = 0; i < sizeLine(); ++i)
        for (int j = 0; j < sizeColumn(); ++j)
            out += get(i, j);

    return out;
}


template <class T>
void Matrix<T>::draw() {

    for (int i = 0; i < sizeLine(); ++i) {
        std::cout << "[ ";
        for (int j = 0; j < sizeColumn(); ++j)
            if (j == sizeColumn() - 1)
                std::cout << get(i, j);
            else
                std::cout << get(i, j) << ", ";
        std::cout << " ]" << std::endl;
    }
}

// ============================================================================
// OPERATORS
// ============================================================================

template <class T>
Matrix<T> Matrix<T>::operator+ (const Matrix<T> & B){
    assert(sizeLine() == B.sizeLine());
    assert(sizeColumn() == B.sizeColumn());

    int nbLine = sizeLine();
    int nbColumn = sizeColumn();

    Matrix<T> sum(nbLine, nbColumn);
    for(int l = 0; l < nbLine; ++l) {
        for(int c = 0; c < nbColumn; ++c) {
            sum.set(l, c, get(l, c) + B.get(l,c));
        }
    }
    return sum;
}


template <class T>
Matrix<T> Matrix<T>::operator- (const Matrix<T> & B){
    assert(sizeLine() == B.sizeLine());
    assert(sizeColumn() == B.sizeColumn());

    int nbLine = sizeLine();
    int nbColumn = sizeColumn();

    Matrix<T> sum(nbLine, nbColumn);
    for(int l = 0; l < nbLine; ++l) {
        for(int c = 0; c < nbColumn; ++c) {
            sum.set(l, c, get(l, c) - B.get(l,c));
        }
    }
    return sum;
}


template <class T>
Matrix<T> Matrix<T>::operator* (const T &val) {
    Matrix<T> result(sizeLine(), sizeColumn());

    for (int l = 0; l < sizeLine(); ++l)
        for (int c = 0; c < sizeColumn(); ++c) {
            T tmp = get(l, c);
            result.set(l, c, tmp * val);
        }

    return result;
}


template <class T>
Matrix<T> Matrix<T>::operator* (const Matrix<T> &B){
    assert(sizeColumn() == B.sizeLine());

    int nbLine = sizeLine();
    int nbColumn = B.sizeColumn();

    Matrix<T> prod(nbLine, nbColumn);
    for(int l = 0; l < nbLine; ++l) {
        for(int c = 0; c < nbColumn; ++c) {

            T tmp=0;
            for(int i=0; i < sizeLine(); ++i){
                tmp += get(l, i) * B.get(i, c);
            }
            prod.set(l, c, tmp);
        }
    }
    return prod;
}



// force gcc to compile for int, float and double
template class Matrix<int>;
template class Matrix<float>;
template class Matrix<double>;
