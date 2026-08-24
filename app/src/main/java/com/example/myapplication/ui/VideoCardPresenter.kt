package com.example.myapplication.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.model.PlaylistItem

class VideoCardPresenter : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.video_card, parent, false)
        return Presenter.ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
        val item = item as PlaylistItem
        val context = viewHolder.view.context

        val thumbnail = viewHolder.view.findViewById<ImageView>(R.id.video_thumbnail)
        val title = viewHolder.view.findViewById<TextView>(R.id.video_title)

        val url = item.snippet?.thumbnails?.medium?.url
        if (!url.isNullOrEmpty()) {
            Glide.with(context)
                .load(url)
                .centerCrop()
                .into(thumbnail)
        }

        title.text = item.snippet?.title ?: ""
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
        val thumbnail = viewHolder.view.findViewById<ImageView>(R.id.video_thumbnail)
        Glide.with(thumbnail.context.applicationContext).clear(thumbnail)
    }
}