function __VERIFIER_error() {}

var obj = {
  __proto__: {
    prop: 77,
    __proto__: {
      prop: 42
    }
  }
};

if (obj.prop !== 77) {
  __VERIFIER_error();
}