package com.github.cvzi.screenshottile.activities

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.collection.LruCache
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.fragments.SettingFragment


/**
 * Shows a how-to tutorial.
 */
class TutorialActivity : AppCompatActivity() {
    companion object {
        /**
         * New Intent for the TutorialActivity
         *
         * @param context    Context
         * @return The intent
         */
        fun newIntent(context: Context): Intent {
            return Intent(context, TutorialActivity::class.java)
        }

        /**
         * Start TutorialActivity
         */
        fun start(ctx: Context, args: Bundle? = null) {
            ctx.startActivity(newIntent(ctx), args)
        }
    }

    private val images = arrayOf(
        R.drawable.screenshot_01,
        R.drawable.screenshot_02,
        R.drawable.screenshot_03,
        R.drawable.screenshot_04,
        R.drawable.screenshot_05,
        R.drawable.screenshot_06,
        R.drawable.screenshot_07,
        R.drawable.screenshot_08,
        R.drawable.screenshot_09,
        R.drawable.screenshot_10,
        R.drawable.screenshot_11,
        R.drawable.screenshot_12,
        R.drawable.screenshot_13,
        R.drawable.screenshot_14,
        R.drawable.screenshot_15,
        R.drawable.screenshot_16
    )

    private val descriptions = arrayOf(
        R.string.tutorial_tap_for_next_step,
        R.string.tutorial_step2,
        R.string.tutorial_step3,
        R.string.tutorial_step4,
        R.string.tutorial_step5,
        R.string.tutorial_step6,
        R.string.tutorial_step6,
        R.string.tutorial_step8,
        R.string.tutorial_step8,
        R.string.tutorial_step10,
        R.string.tutorial_step11,
        R.string.tutorial_step11,
        R.string.tutorial_step11,
        R.string.tutorial_step11,
        R.string.tutorial_step11,
        R.string.tutorial_step16
    )

    private lateinit var bitmapCache: BitmapCache

    private lateinit var viewPager: ViewPager
    private lateinit var textViewStep: TextView
    private lateinit var textViewFooter: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial)

        val cacheSize = (Runtime.getRuntime().maxMemory() / 1024).toInt() / 4
        bitmapCache = BitmapCache(cacheSize)

        textViewStep = findViewById(R.id.textViewStep)
        textViewFooter = findViewById(R.id.textViewFooter)

        viewPager = findViewById(R.id.viewPager)
        viewPager.adapter = TutorialPagerAdapter()

        if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
            // Night Mode -> Background is black so transparency makes the images darker
            viewPager.alpha = 0.7f
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Disable back gesture (slide from right edge to left) on viewPager
            viewPager.systemGestureExclusionRects =
                listOf(Rect(0, 0, viewPager.width, viewPager.height))
            viewPager.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                viewPager.systemGestureExclusionRects = listOf(Rect(0, 0, v.width, v.height))
            }
        }

        viewPager.setOnClickListener {
            viewPager.setCurrentItem(viewPager.currentItem + 1 % images.size, true)
        }
        viewPager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
                // no-op
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                // no-op
            }

            override fun onPageSelected(position: Int) {
                textViewStep.text = (position + 1).toString()
                textViewFooter.text = if (position >= 0 && position < descriptions.size) {
                    getString(descriptions[position])
                } else {
                    getString(descriptions[0])
                }
            }
        })

        findViewById<Button>(R.id.buttonSettings)?.setOnClickListener {
            ContainerActivity.start(this, SettingFragment::class.java)
        }
    }

    private inner class ClickableImageView(context: Context) : AppCompatImageView(context) {
        init {
            setOnClickListener {
                viewPager.setCurrentItem((viewPager.currentItem + 1) % images.size, true)
            }
        }
    }

    private inner class TutorialPagerAdapter : PagerAdapter() {
        override fun isViewFromObject(view: View, obj: Any): Boolean {
            return view == obj
        }

        override fun getCount(): Int {
            return images.size
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            return ClickableImageView(this@TutorialActivity).apply {
                setImageBitmap(bitmapCache.get(images[position]))
                (container as? ViewPager)?.addView(this, 0)
            }
        }

        override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
            (container as? ViewPager)?.removeView(obj as? View)
        }
    }

    private inner class BitmapCache(cacheSize: Int) : LruCache<Int, Bitmap>(cacheSize) {
        override fun sizeOf(resId: Int, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }

        override fun create(resId: Int): Bitmap? {
            return BitmapFactory.decodeResource(resources, resId)
        }
    }
}
