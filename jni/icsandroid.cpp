
#include "icsandroid.h"
#include "client/linux/handler/exception_handler.h"

namespace {

bool DumpCallback(const google_breakpad::MinidumpDescriptor& descriptor,
                  void* context,
                  bool succeeded) {
  printf("Dump path: %s\n", descriptor.path());
fflush(stdout);
  return succeeded;
}

void Crash() {
  volatile int* a = reinterpret_cast<volatile int*>(NULL);
  *a = 1;
}

}  // namespace

static google_breakpad::MinidumpDescriptor* desc;
static google_breakpad::ExceptionHandler* eh;
void setup_breakpad(void)
{
printf("Initializing Google Breakpad!\n");
desc = new google_breakpad::MinidumpDescriptor("/data/data/de.blinkt.openvpn/cache");
eh = new google_breakpad::ExceptionHandler(*desc, NULL, DumpCallback, NULL, true,-1);
}

