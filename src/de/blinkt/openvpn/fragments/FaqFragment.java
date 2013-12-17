package de.blinkt.openvpn.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import de.blinkt.openvpn.R;

public class FaqFragment extends Fragment  {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
    		Bundle savedInstanceState) {
    	View v= inflater.inflate(R.layout.faq, container, false);
    	
    	insertHtmlEntry(v,R.id.broken_images_faq,R.string.broken_images_faq);
    	insertHtmlEntry(v,R.id.faq_howto,R.string.faq_howto);
    	insertHtmlEntry(v, R.id.baterry_consumption, R.string.baterry_consumption);  
    	insertHtmlEntry(v, R.id.faq_tethering, R.string.faq_tethering);
        insertHtmlEntry(v, R.id.faq_vpndialog43, R.string.faq_vpndialog43);
        insertHtmlEntry(v, R.id.faq_system_dialog_xposed, R.string.faq_system_dialog_xposed);
		return v;
    }

	private void insertHtmlEntry (View v, int viewId, int stringId) {
		TextView faqitem = (TextView) v.findViewById(viewId);
    	faqitem.setText(Html.fromHtml(getActivity().getString(stringId)));
    	faqitem.setMovementMethod(LinkMovementMethod.getInstance());

	}

}
