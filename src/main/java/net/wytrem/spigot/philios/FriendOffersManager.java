package net.wytrem.spigot.philios;

import net.wytrem.spigot.utils.WyPlugin;
import net.wytrem.spigot.utils.offers.OffersManager;
import org.bukkit.entity.Player;

public class FriendOffersManager extends OffersManager<FriendOffer> {
    public FriendOffersManager(WyPlugin plugin) {
        super(plugin);
    }

    @Override
    protected FriendOffer createDefaultOffer(Player sender, Player recipient) {
        return new FriendOffer(sender, recipient);
    }

    @Override
    public void post(FriendOffer offer) {
        if (Philios.instance.getFriendships().areFriends(offer.getSender(), offer.getRecipient())) {
            Philios.instance.texts.youAreAlreadyFriendWithOther.format("player", offer.getRecipient()).send(offer.getSender());
        }
        else {
            super.post(offer);
        }
    }
}
