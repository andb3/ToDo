package com.andb.apps.todo.views

import android.content.Context
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import com.andb.apps.todo.R
import com.andb.apps.todo.Utilities
import com.jaredrummler.cyanea.Cyanea

class Icon(context: Context, attributeSet: AttributeSet) : AppCompatImageView(context, attributeSet){

    init {
        context.theme.obtainStyledAttributes(attributeSet, R.styleable.Icon, 0, 0).apply {
            try {
                setCyaneaBackground(getInteger(R.styleable.Icon_cyaneaIconBackground, 0), this@Icon.alpha)
            }finally {
                recycle()
            }
        }

    }

    fun setCyaneaBackground(bg: Int, alpha: Float){
        when(bg){
            0-> setColorFilter(
                    if(Utilities.lightOnBackground(Cyanea.instance.backgroundColor))
                        Utilities.colorWithAlpha(Color.WHITE, alpha)
                    else
                        Utilities.colorWithAlpha(Color.BLACK, alpha))
            1-> setColorFilter(
                    if(Utilities.lightOnBackground(Cyanea.instance.primary))
                        Utilities.colorWithAlpha(Color.WHITE, alpha)
                    else
                        Utilities.colorWithAlpha(Color.BLACK, alpha))
            2-> setColorFilter(
                    if(Utilities.lightOnBackground(Cyanea.instance.accent))
                        Utilities.colorWithAlpha(Color.WHITE, alpha)
                    else Utilities.colorWithAlpha(Color.BLACK, alpha))
        }
    }

}