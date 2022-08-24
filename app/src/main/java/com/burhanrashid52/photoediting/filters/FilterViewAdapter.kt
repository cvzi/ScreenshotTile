package com.burhanrashid52.photoediting.filters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.cvzi.screenshottile.R
import ja.burhanrashid52.photoeditor.PhotoFilter
import java.io.IOException

/**
 * @author [Burhanuddin Rashid](https://github.com/burhanrashid52)
 * @version 0.1.2
 * @since 5/23/2018
 */
class FilterViewAdapter(private val mFilterListener: FilterListener) :
    RecyclerView.Adapter<FilterViewAdapter.ViewHolder>() {
    private val mPairList: MutableList<Pair<String, PhotoFilter>> = ArrayList()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.row_filter_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val filterPair = mPairList[position]
        val fromAsset = getBitmapFromAsset(holder.itemView.context, filterPair.first)
        holder.mImageFilterView.setImageBitmap(fromAsset)
        holder.mTxtFilterName.text = filterPair.second.name.replace("_", " ")
    }

    override fun getItemCount(): Int {
        return mPairList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mImageFilterView: ImageView = itemView.findViewById(R.id.imgFilterView)
        val mTxtFilterName: TextView = itemView.findViewById(R.id.txtFilterName)

        init {
            itemView.setOnClickListener {
                mFilterListener.onFilterSelected(
                    mPairList[layoutPosition].second
                )
            }
        }
    }

    private fun getBitmapFromAsset(context: Context, strName: String): Bitmap? {
        val assetManager = context.assets
        return try {
            val istr = assetManager.open(strName)
            BitmapFactory.decodeStream(istr)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun setupFilters() {
        mPairList.add(Pair("filters/original.webp", PhotoFilter.NONE))
        mPairList.add(Pair("filters/auto_fix.webp", PhotoFilter.AUTO_FIX))
        mPairList.add(Pair("filters/brightness.webp", PhotoFilter.BRIGHTNESS))
        mPairList.add(Pair("filters/contrast.webp", PhotoFilter.CONTRAST))
        mPairList.add(Pair("filters/documentary.webp", PhotoFilter.DOCUMENTARY))
        mPairList.add(Pair("filters/due_tone.webp", PhotoFilter.DUE_TONE))
        mPairList.add(Pair("filters/fill_light.webp", PhotoFilter.FILL_LIGHT))
        mPairList.add(Pair("filters/fish_eye.webp", PhotoFilter.FISH_EYE))
        mPairList.add(Pair("filters/grain.webp", PhotoFilter.GRAIN))
        mPairList.add(Pair("filters/gray_scale.webp", PhotoFilter.GRAY_SCALE))
        mPairList.add(Pair("filters/lomish.webp", PhotoFilter.LOMISH))
        mPairList.add(Pair("filters/negative.webp", PhotoFilter.NEGATIVE))
        mPairList.add(Pair("filters/posterize.webp", PhotoFilter.POSTERIZE))
        mPairList.add(Pair("filters/saturate.webp", PhotoFilter.SATURATE))
        mPairList.add(Pair("filters/sepia.webp", PhotoFilter.SEPIA))
        mPairList.add(Pair("filters/sharpen.webp", PhotoFilter.SHARPEN))
        mPairList.add(Pair("filters/temperature.webp", PhotoFilter.TEMPERATURE))
        mPairList.add(Pair("filters/tint.webp", PhotoFilter.TINT))
        mPairList.add(Pair("filters/vignette.webp", PhotoFilter.VIGNETTE))
        mPairList.add(Pair("filters/cross_process.webp", PhotoFilter.CROSS_PROCESS))
        mPairList.add(Pair("filters/b_n_w.webp", PhotoFilter.BLACK_WHITE))
        mPairList.add(Pair("filters/flip_horizontal.webp", PhotoFilter.FLIP_HORIZONTAL))
        mPairList.add(Pair("filters/flip_vertical.webp", PhotoFilter.FLIP_VERTICAL))
        mPairList.add(Pair("filters/rotate.webp", PhotoFilter.ROTATE))
    }

    init {
        setupFilters()
    }
}