package de.blinkt.vpndialogxposed;

import android.Manifest;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Vector;


public class AllowedVPNsChooser extends ListActivity {

    public static final String ALLOWED_APPS = "allowedApps";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Collection<VpnApp> vpnApps = getVpnAppList();
        ListAdapter la = new ArrayAdapter<VpnApp>(this, android.R.layout.simple_list_item_multiple_choice, vpnApps.toArray(new VpnApp[vpnApps.size()]));
        setListAdapter(la);
        setContentView(R.layout.vpnapplayout);
        getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);


        Collection<String> allowedapps = getAllowedApps();
        for(int i=0; i < vpnApps.size(); i++) {
            VpnApp va= (VpnApp) getListView().getItemAtPosition(i);
            boolean allowed = allowedapps.contains(va.mPkg);
            getListView().setItemChecked(i,allowed);
        }
        
    }



    private Collection<String> getAllowedApps(){
        @SuppressWarnings("deprecation")  SharedPreferences prefs = getPreferences(MODE_WORLD_READABLE);
        HashSet<String> defaultapps = new HashSet<String>();
        defaultapps.add("de.blinkt.openvpn");
        return prefs.getStringSet(ALLOWED_APPS,defaultapps );
    }

    private void saveAllowedApps(Set<String> allowedApps)
    {
        @SuppressWarnings("deprecation") SharedPreferences prefs = getPreferences(MODE_WORLD_READABLE);
        SharedPreferences.Editor prefeditor = prefs.edit();
        prefeditor.putStringSet(ALLOWED_APPS,allowedApps);
        prefeditor.putInt("random",new Random().nextInt());
        prefeditor.apply();
    }


    @Override
    protected void onStop() {
        super.onStop();

        HashSet<String> allowedPkgs= new HashSet<String>();
        for(int i=0;i < getListView().getChildCount();i++) {
            if(getListView().getCheckedItemPositions().get(i)) {
                allowedPkgs.add(((VpnApp)getListView().getItemAtPosition(i)).mPkg);
            }
        }
        saveAllowedApps(allowedPkgs);
    }

    private Collection<VpnApp> getVpnAppList() {
        PackageManager pm = getPackageManager();
        Intent vpnOpen = new Intent();
        vpnOpen.setAction("android.net.VpnService");
        Vector<VpnApp> vpnApps = new Vector<VpnApp>();

        // Hack but should work
        for(PackageInfo pkg : pm.getInstalledPackages(PackageManager.GET_SERVICES)) {
            if (pkg.services != null) {
                for(ServiceInfo serviceInfo:pkg.services) {
                    if(Manifest.permission.BIND_VPN_SERVICE.equals(serviceInfo.permission))
                        vpnApps.add(new VpnApp(pkg.applicationInfo.loadLabel(pm), pkg.packageName));
                }
            }
        }


        /*     public abstract List<ResolveInfo> queryIntentServicesAsUser(Intent intent,
            int flags, int userId);
        */

            /* This does not work ... */
            /*
        Class<?>[] queryIntentServicesAsUserSignature = {Intent.class, int.class, int.class};
        try {
            Method queryIntentServicesAsUser = pm.getClass().getMethod("queryIntentServicesAsUser", queryIntentServicesAsUserSignature);

            List<ApplicationInfo> installedApps = pm.getInstalledApplications(0);



            for (ApplicationInfo app : installedApps) {

                List<ResolveInfo> apps;
                if (app.packageName.equals(getPackageName())) {
                    apps = pm.queryIntentServices(vpnOpen, 0);
                } else {
                    apps = (List<ResolveInfo>) queryIntentServicesAsUser.invoke(pm, vpnOpen, 0, app.uid);
                }



                for (ResolveInfo ri : apps) {
                    vpnApps.add(new VpnApp(ri.toString()));
                }
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    */

        return vpnApps;
    }

    static class VpnApp {

        private final String mPkg;
        private CharSequence mName;

        public VpnApp(CharSequence name, String pkg) {
            mName = name;
            mPkg = pkg;
        }

        @Override
        public String toString() {
            return mName.toString();
        }
    }
}
