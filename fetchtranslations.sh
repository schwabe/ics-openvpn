#! /bin/sh

fetch http://crowdin.net/download/project/ics-openvpn.zip

langtoinclude="de cs ko et fr"

for lang in $langtoinclude
do
    tar xfv ics-openvpn.zip /res/values-$lang/
done