package com.applicaster.quickbrickplayerplugin.helper

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.util.TypedValue
import androidx.core.content.res.ResourcesCompat
import com.applicaster.quickbrickplayerplugin.R
import com.applicaster.session.SessionStorage
import com.kaltura.android.exoplayer2.ui.PlayerView
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.android.synthetic.main.player_control_view.view.*

/***
 *      ZappFonts class set the font, size, and color for
 *      player title and player summary
 *
 *      class follows two rules
 *      1) check if json contains the key
 *      2) fallback to default value if json doesn't have key or if system doesn't have font install
 *
 *
 *      @author  Raymond Williams
 *      @version 1.0
 *      @since   2020-01-13
 */
class ZappFonts(
    playerView: PlayerView,
    context: Context
) {

    private val session = SessionStorage.getAll("QuickBrickPlayerPlugin")
    private var jsonData: JsonObject? = null

    private val titleFontKey = "video_player_title_font"
    private val titleSizeKey = "video_player_title_font_size"
    private val titleColorKey = "video_player_title_color"

    private val summaryFontKey = "video_player_summary_font"
    private val summarySizeKey = "video_player_summary_font_size"
    private val summaryColorKey = "video_player_summary_color"

    private val progressBarPlayedColorKey = "progress_bar_played_color"

    init {
        jsonData = JsonParser().parse(session).asJsonObject
        Log.d("ZappFonts", jsonData.toString())
        setTextFont(playerView)
        setTextColor(playerView)
        setFont(playerView, context)
        setUpProgressBarConfig(playerView)
    }

    private fun setUpProgressBarConfig(playerView: PlayerView) {
        val playColor = if(hasKey(progressBarPlayedColorKey)) jsonData!![progressBarPlayedColorKey].asString else  "#fc461b"

        //playerView.exo_progress.setPlayedColor(Color.parseColor(playColor))
    }

    private fun setTextColor(playerView: PlayerView) {
        val titleColor = if (hasKey(titleColorKey)) jsonData!![titleColorKey].asString else "#EFEFEF"
        val summaryColor = if (hasKey(summaryColorKey)) jsonData!![summaryColorKey].asString else "#EFEFEF"
        playerView.player_title.setTextColor(Color.parseColor(titleColor))
        playerView.player_description.setTextColor(Color.parseColor(summaryColor))
    }

    private fun setTextFont(playerView: PlayerView) {
        val titleFontSize = if (hasKey(titleSizeKey)) jsonData!![titleSizeKey].asString.toFloat() else 60.0f
        val summaryFontSize = if (hasKey(summarySizeKey)) jsonData!![summarySizeKey].asString.toFloat() else 25.0f

        playerView.player_title.setTextSize(TypedValue.COMPLEX_UNIT_SP, titleFontSize)
        playerView.player_description.setTextSize(TypedValue.COMPLEX_UNIT_SP, summaryFontSize)
    }

    private fun setFont(playerView: PlayerView, context: Context) {
        val titleTypeface: Typeface? = if (hasKey(titleFontKey))
            try {
                Log.d("titleTypeface", getFontFromSystem(titleFontKey))
                Typeface.createFromAsset(context.assets, "fonts/${getFontFromSystem(titleFontKey)}.ttf")
            } catch (e: RuntimeException) {
                Log.d("titleTypeface catch", "roboto_bold.ttf")
                ResourcesCompat.getFont(context, R.font.roboto_bold)
                //                    Typeface.createFromAsset(context.assets, "font/montserrat_bold.ttf")
            }
        else {
            Log.d("titleTypeface else", "roboto_bold.ttf")
            ResourcesCompat.getFont(context, R.font.roboto_bold)
        }

        val summaryTypeface: Typeface? = if (hasKey(summaryFontKey))
            try {
                Log.d("summaryTypeface", getFontFromSystem(summaryFontKey))
                Typeface.createFromAsset(context.assets, "font/${getFontFromSystem(summaryFontKey)}.ttf")
            } catch (e: RuntimeException) {
                Log.d("summaryTypeface catch", "roboto.ttf")
                ResourcesCompat.getFont(context, R.font.roboto)
            }
        else {
            Log.d("summaryTypeface else", "roboto.ttf")
            ResourcesCompat.getFont(context, R.font.roboto)
        }

        playerView.player_title.typeface = titleTypeface
        playerView.player_description.typeface = summaryTypeface
    }


    /**
     *  return font name from Json
     */
    private fun getFontFromSystem(fontKey: String): String {
        return jsonData!![fontKey].asString
    }


    /**
     *  return boolean if json has key
     */
    private fun hasKey(keyName: String): Boolean {
        return jsonData!!.has(keyName)
    }
}