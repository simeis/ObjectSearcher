package com.simeiscomet.objectsearcher;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class SenderActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sender);

        final BitmapApplication app = (BitmapApplication)this.getApplication();

        final EditText editOwner = (EditText)findViewById(R.id.editOwner);
        final EditText editCategory = (EditText)findViewById(R.id.editCategory);
        final EditText editType = (EditText)findViewById(R.id.editType);
        final EditText editCompany = (EditText)findViewById(R.id.editCompany);
        final EditText editProduct = (EditText)findViewById(R.id.editProduct);
        final CheckBox checkFailure = (CheckBox)findViewById(R.id.checkFailure);

        final ImageView imageObject = (ImageView)findViewById(R.id.imageObject);

        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // 入力内容の初期化
        editOwner.setText( pref.getString("owner", "") );
        editCategory.setText("");
        editType.setText("");
        editCompany.setText("NULL");
        editProduct.setText("NULL");
        //checkFailure.setText("失敗している");
        //checkFailure.setChecked(false);

        imageObject.setImageBitmap( app.getObj() );

        final Button button = (Button)findViewById(R.id.buttonSend);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean complete = true;
                String toastText = "";
                if( editOwner.getText().toString().length() <= 0 ){
                    toastText += "所有者  ";
                    complete = false;
                }
                if( editCategory.getText().toString().length() <= 0 ){
                    toastText += "グループ  ";
                    complete = false;
                }
                if( editType.getText().toString().length() <= 0 ){
                    toastText += "物体の種類  ";
                    complete = false;
                }
                if( !complete ){
                    toastText += "は入力必須です";
                    Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_SHORT).show();
                    return;
                }

                SharedPreferences.Editor e = pref.edit();
                e.putString("owner", editOwner.getText().toString() );
                e.commit();

                String[] objInfo = new String[6];
                objInfo[0] = editOwner.getText().toString();
                objInfo[1] = editCategory.getText().toString();
                objInfo[2] = editType.getText().toString();
                objInfo[3] = editCompany.getText().toString();
                objInfo[4] = editProduct.getText().toString();
                objInfo[5] = ( checkFailure.isChecked() ? "true" : "false" );

                _sendImage( app.getObj(), objInfo );

                //app.clearObj();
            }
        });
    }

    private void _sendImage( Bitmap image, String[] objInfo ){
        try{
            new HttpMultiPostAsync( this, getApplicationContext().getString( R.string.php_url ), image, objInfo ){
                @Override
                protected void onSuccess( Boolean result )
                {
                    Intent intent = new Intent( getApplicationContext(), Main.class );
                    //intent.putExtra( "objInfo", objInfo );
                    intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK );
                    getApplicationContext().startActivity( intent );
                }
            }.execute();
        } catch ( RuntimeException e ){
            Log.e("Error", "" + e.toString());
        }
    }
}
