package utb.desarrollomovil.ubicacionutb;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class CategoriasActivity extends AppCompatActivity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categorias);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        linearLayoutDetails=(ViewGroup) findViewById(R.id.linearLayoutDetails);
        linearLayoutDetails1=(ViewGroup) findViewById(R.id.linearLayoutDetails1);
        linearLayoutDetails2=(ViewGroup) findViewById(R.id.linearLayoutDetails2);
        imageViewExpand=(ImageView) findViewById(R.id.imageViewExpand);
        imageViewExpand1=(ImageView) findViewById(R.id.imageViewExpand1);
        imageViewExpand2=(ImageView) findViewById(R.id.imageViewExpand2);

        l=(LinearLayout) findViewById(R.id.layout_option_uno);
        l1=(LinearLayout)findViewById(R.id.layout_option_dos);
        l2=(LinearLayout)findViewById(R.id.layout_option_tres);
        l.setOnClickListener(this);
        l1.setOnClickListener(this);
        l2.setOnClickListener(this);
    }
    private LinearLayout l,l1,l2;
    private ViewGroup linearLayoutDetails,linearLayoutDetails1,linearLayoutDetails2;
    private ImageView imageViewExpand,imageViewExpand1,imageViewExpand2;
    private static final int DURATION = 250;



    private void rotate(float angle,ImageView v) {
        Animation animation = new RotateAnimation(0.0f, angle, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        animation.setFillAfter(true);
        animation.setDuration(DURATION);
        v.startAnimation(animation);
    }

    @Override
    public void onClick(View view) {
        if(view.getId()==R.id.layout_option_uno){

            if (linearLayoutDetails.getVisibility() == View.GONE) {
                ExpandAndCollapseViewUtil.expand(linearLayoutDetails, DURATION);
                imageViewExpand.setImageResource(R.drawable.flecha_abajo_categorias);
                rotate(-180.0f,imageViewExpand);
            } else {
                ExpandAndCollapseViewUtil.collapse(linearLayoutDetails, DURATION);
                imageViewExpand.setImageResource(R.drawable.flecha_abajo_categorias);
                rotate(0.0f,imageViewExpand);
            }
        }
        else if(view.getId()==R.id.layout_option_dos){

            if (linearLayoutDetails1.getVisibility() == View.GONE) {
                ExpandAndCollapseViewUtil.expand(linearLayoutDetails1, DURATION);
                imageViewExpand1.setImageResource(R.drawable.flecha_abajo_categorias);
                rotate(-180.0f,imageViewExpand1);
            } else {
                ExpandAndCollapseViewUtil.collapse(linearLayoutDetails1, DURATION);
                imageViewExpand1.setImageResource(R.drawable.flecha_abajo_categorias);
                rotate(0.0f,imageViewExpand1);
            }
        }
        else if(view.getId()==R.id.layout_option_tres){

            if (linearLayoutDetails2.getVisibility() == View.GONE) {
                ExpandAndCollapseViewUtil.expand(linearLayoutDetails2, DURATION);
                imageViewExpand2.setImageResource(R.drawable.flecha_abajo_categorias);
                rotate(-180.0f,imageViewExpand2);
            } else {
                ExpandAndCollapseViewUtil.collapse(linearLayoutDetails2, DURATION);
                imageViewExpand2.setImageResource(R.drawable.flecha_abajo_categorias);
                rotate(0.0f,imageViewExpand2);
            }
        }
    }
}