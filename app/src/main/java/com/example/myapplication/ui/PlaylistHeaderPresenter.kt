package com.example.myapplication.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.RowHeaderPresenter
import com.example.myapplication.R

class PlaylistHeaderPresenter : RowHeaderPresenter() {

    class ViewHolder(view: View) : RowHeaderPresenter.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.header_title)
    }

    override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.playlist_header, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any?) {
        val holder = viewHolder as ViewHolder
        val headerItem = item as? HeaderItem
        holder.title.text = headerItem?.name ?: ""
        holder.view.alpha = 0.55f
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder?) {}

    override fun onSelectLevelChanged(viewHolder: RowHeaderPresenter.ViewHolder?) {
        val holder = viewHolder as? ViewHolder ?: return
        holder.view.alpha = 0.55f + 0.45f * viewHolder.selectLevel
    }
}
