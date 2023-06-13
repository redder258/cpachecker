// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int VERDICT_SAFE;
int CURRENTLY_UNSAFE;

#include <assert.h>
int main(void) {
	int *a,*b;
	b = a;
	*a = 2;
	assert(*b==2);
	return 0;
}
