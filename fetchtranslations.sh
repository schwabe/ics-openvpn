#! /bin/sh


if [ "$ICSCROWDAPIKEY" != "" ]
then
	fetch -1 -o - http://api.crowdin.net/api/project/ics-openvpn/export?key=$ICSCROWDAPIKEY
fi

fetch http://crowdin.net/download/project/ics-openvpn.zip

langtoinclude="de cs ko et fr he"

for lang in $langtoinclude
do
    tar xfv ics-openvpn.zip /res/values-$lang/
done

# Chinese language require zh-CN and zh-TW

for lang in zh-CN
do
	fetch http://crowdin.net/download/project/ics-openvpn/$lang.zip
	tar -xv -C res/values-$lang/ --strip-components 3 -f $lang.zip
done