/**
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package no.barentswatch.implementation;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ListContentFragment extends Fragment {
    private String mText;

    @Override
    public void onAttach(Activity activity) {
      // This is the first callback received; here we can set the text for
      // the fragment as defined by the tag specified during the fragment
      // transaction
      super.onAttach(activity);
      mText = getTag();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // This is called to define the layout for the fragment;
        // we just create a TextView and set its text to be the fragment tag
        TextView text = new TextView(getActivity());
        text.setText(mText);
        return text;
    }
}
