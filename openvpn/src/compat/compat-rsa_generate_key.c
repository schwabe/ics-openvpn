#include <stdio.h>
#include <time.h>
#include "cryptlib.h"
#include <openssl/bn.h>
#include <openssl/rsa.h>

RSA *RSA_generate_key(int bits, unsigned long e_value,
		      void (*callback)(int,int,void *), void *cb_arg)
{
  BN_GENCB cb;
  int i;
  RSA *rsa = RSA_new();
  BIGNUM *e = BN_new();

  if(!rsa || !e) goto err;

  /* The problem is when building with 8, 16, or 32 BN_ULONG,
   * unsigned long can be larger */
  for (i=0; i<(int)sizeof(unsigned long)*8; i++)
    {
      if (e_value & (1UL<<i))
	if (BN_set_bit(e,i) == 0)
	  goto err;
    }

  BN_GENCB_set_old(&cb, callback, cb_arg);

  if(RSA_generate_key_ex(rsa, bits, e, &cb)) {
    BN_free(e);
    return rsa;
  }
 err:
  if(e) BN_free(e);
  if(rsa) RSA_free(rsa);
  return 0;
}



void mlockall(){}
char *
getpass (prompt)
     const char *prompt;
{
  return "";
}

