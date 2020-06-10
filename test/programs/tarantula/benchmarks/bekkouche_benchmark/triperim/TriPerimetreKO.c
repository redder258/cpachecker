/*
The program TriPerimetre (implemented in the function foo) 
has the exact same control structure of Tritype. The difference 
is that TriPerimetre returns a calculation on the input and not 
a constant value. The program returns the sum of the triangle 
sides if the input conrresponds to a valid triangle; and 
returns -1 otherwise. 

THe error is this program is in the assignement "res = 2*i + k", 
the instruction should be "res = 2*i+j".
By taking the input {i=2,j=1,k=2}, the program returns as a sum of 
triangle sides the value 6, however, it should return the value 5. 
All the conditions in this program are correct. 

SPDX-FileCopyrightText: Mohammed Bekkouche <http://www.i3s.unice.fr>
SPDX-License-Identifier: Apache-2.0
*/

extern int __VERIFIER_nondet_uint();
extern void __VERIFIER_error();


void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: __VERIFIER_error();
  }
  return;
} 
/* program for triangle perimeter 
 * returns i+j+k
 *      i+j+k if (i,j,k) are the sides of any triangle
 *      2*i + j or 2*i+k or 2*j+i if (i,j,k) are the sides of an isosceles triangle
 *      3*i if (i,j,k) are the sides of an equilateral triangle
 * returns -1 if (i,j,k) are not the sides of a triangle
 * 
 * ERROR : in assignment line 58
 * returns 6 for (2,1,2) while it should return 5
 */
void foo (int i, int j, int k) {
    int trityp = 0;
    int res;
    if (i == 0 || j == 0 || k == 0) {
        trityp = 4;	
        res = -1;
    }
    else {
        trityp = 0;
        if (i == j) {
            trityp = trityp + 1;
        }
        if (i == k) {
            trityp = trityp + 2;
        }
        if (j == k) {
            trityp = trityp + 3;
        }
        if (trityp == 0) {
            if ((i+j) <= k || ((j+k) <= i || (i+k) <= j)) {
                trityp = 4;
                res = -1;
            }
            else {
                trityp = 1;
                res = i+j+k;
            }			
        }
        else {
            if (trityp > 3) {
                res = 3*i;
            }
            else {
                if (trityp == 1 && (i+j) > k) { // i==j
                    res=2*i+k; 
                }
                else {
                    // error in the assignment : should be res = 2*i+j
                    if (trityp == 2 && (i+k) > j) {    // i==k
                        res = 2*i + k; // BUG
                    }
                    else {
                        if (trityp == 3 && (j+k) > i) {   // j==k
                            res=2*j+i;
                        }
                        else {
                            res=-1;
                        }
                    }
                }
            }
        }
    }

    
    __VERIFIER_assert(res== ((i==0 || j==0 || k==0)?-1:(  (i!=j && i!=k && j!=k)?(   ((i+j)<=k || (j+k)<=i || (i+k)<=j)?-1:(i+j+k)   ):( ((i==j && j==k) || (j==k && i==k))?(3*i):( (i==j && i!=k && j!=k && (i+j)>k)?(2*i+k):( (i!=j && j!=k && i==k && (i+k)>j)?(2*i+j):( ( ((i!=j && j==k && i!=k) || (i==j && j!=k && i==k)) && (j+k)>i)?(2*j+i):-1 ) ) ) ) )) );
    
}


int main() 
{ 
  
  foo( __VERIFIER_nondet_int(),__VERIFIER_nondet_int(),__VERIFIER_nondet_int());
    return 0; 
} 