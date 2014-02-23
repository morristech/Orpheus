/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.ui.cards.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.PopupMenu;

import com.andrew.apollo.R;

import org.opensilk.music.ui.cards.CardBaseList;

import it.gmariotti.cardslib.library.view.CardView;

/**
 * Created by drew on 2/22/14.
 */
public class CardViewNoHeader extends CardView {

    protected View mInternalOverflowButton;

    public CardViewNoHeader(Context context) {
        super(context);
    }

    public CardViewNoHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CardViewNoHeader(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void retrieveLayoutIDs() {
        super.retrieveLayoutIDs();
        mInternalOverflowButton = (View) findViewById(R.id.card_overflow_button);
    }

    @Override
    protected void setupListeners() {
        super.setupListeners();
        setupOverflowButtonListener();
    }

    protected void setupOverflowButtonListener() {
        mInternalOverflowButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu menu = new PopupMenu(v.getContext(), v);
                menu.inflate(((CardBaseList) getCard()).getOverflowMenuId());
                menu.setOnMenuItemClickListener(((CardBaseList) getCard()).getOverflowPopupMenuListener());
                menu.show();
            }
        });
    }
}