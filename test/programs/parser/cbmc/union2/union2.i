# 1 "union2/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "union2/main.c"
struct hh{
  unsigned short h1;
  unsigned short h2;
};
union uuu {
  unsigned int l;
  struct hh h;
};

int main()
{
  union uuu ebx;
  ebx.l=10;
  assert(ebx.l<1000);
}
