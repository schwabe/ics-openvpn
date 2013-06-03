#! /bin/sh


if [ "$ICSCROWDAPIKEY" != "" ]
then
	echo "Generating new translation archives"
	fetch -q -1 -o - http://api.crowdin.net/api/project/ics-openvpn/export?key=$ICSCROWDAPIKEY
fi

echo "Fetch translation archive"
fetch -q http://crowdin.net/download/project/ics-openvpn.zip

langtoinclude="ca cs de es et fr id it ja ko no nl pl ro ru sv uk"

for lang in $langtoinclude
do
    tar xfv ics-openvpn.zip res/values-$lang/
done

# Chinese language require zh-CN and zh-TW

for lang in zh-CN zh-TW id
do
	if [ $lang = "zh-CN" ] ; then
		rlang="zh-rCN"
    elif [ $lang = "zh-TW" ] ; then
        rlang="zh-rTW"
    elif [ $lang = "id" ] ; then
        rlang="id"
	fi

	echo "Fetch archive for $lang"
	fetch http://crowdin.net/download/project/ics-openvpn/$lang.zip
	tar -xv -C res/values-$rlang/ --strip-components 3 -f $lang.zip
    rm $lang.zip
done

rm -v ics-openvpn.zip
