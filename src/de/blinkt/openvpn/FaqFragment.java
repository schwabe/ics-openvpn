package de.blinkt.openvpn;

import android.app.Fragment;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class FaqFragment extends Fragment  {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
    		Bundle savedInstanceState) {
    	View v= inflater.inflate(R.layout.faq, container, false);
    	
    	TextView bImages = (TextView) v.findViewById(R.id.brokenimages);
    	bImages.setText(Html.fromHtml(getActivity().getString(R.string.broken_images_faq)));
    	bImages.setMovementMethod(LinkMovementMethod.getInstance());
    	
    	TextView quickstart = (TextView) v.findViewById(R.id.faq_howto);
    	Spanned htmltext = Html.fromHtml(getActivity().getString(R.string.faq_howto));
    	quickstart.setText(htmltext);
    	quickstart.setMovementMethod(LinkMovementMethod.getInstance());
		
		return v;
		
		

    }

}
