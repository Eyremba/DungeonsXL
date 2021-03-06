/*
 * Copyright (C) 2012-2016 Frank Baumann
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.dre2n.dungeonsxl.event.dgroup;

import io.github.dre2n.dungeonsxl.player.DGroup;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * @author Daniel Saukel
 */
public class DGroupScoreEvent extends DGroupEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;

    private Player scorer;
    private DGroup loserGroup;

    public DGroupScoreEvent(DGroup dGroup, Player scorer, DGroup loserGroup) {
        super(dGroup);
        this.scorer = scorer;
        this.loserGroup = loserGroup;
    }

    /**
     * @return the creator
     */
    public Player getScorer() {
        return scorer;
    }

    /**
     * @param scorer
     * the scoerer to set
     */
    public void setCreator(Player scorer) {
        this.scorer = scorer;
    }

    /**
     * @return the group that lost a score to the scorers
     */
    public DGroup getLoserGroup() {
        return loserGroup;
    }

    /**
     * @param loserGroup
     * the group that lost a score to the scorers to set
     */
    public void setLoserGroup(DGroup loserGroup) {
        this.loserGroup = loserGroup;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

}
