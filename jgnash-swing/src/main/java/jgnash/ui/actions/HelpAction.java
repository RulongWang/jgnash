/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.ui.actions;

import javax.help.HelpBroker;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

import jgnash.ui.UIApplication;
import jgnash.ui.util.builder.Action;

/**
 * Action to display help tracker
 *
 * @author Craig Cavanaugh
 * @version $Id: HelpAction.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */

@Action("help-command")
public final class HelpAction extends AbstractAction {

    private static final long serialVersionUID = 299645809136156798L;    

    /**
     * Invoked when an action occurs.
     *
     * @param e action event
     */
    @Override
    public void actionPerformed(final ActionEvent e) {

        HelpBroker broker = UIApplication.getHelpBroker();

        if (broker != null) {                               
            UIApplication.showHelp(UIApplication.INTRODUCTION_ID);
        } else {
            setEnabled(false); // disable completely
        }

    }
}