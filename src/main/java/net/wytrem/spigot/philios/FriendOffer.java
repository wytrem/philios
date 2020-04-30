package net.wytrem.spigot.philios;

import net.wytrem.spigot.utils.offers.Offer;
import org.bukkit.entity.Player;

public class FriendOffer extends Offer {
    public FriendOffer(Player sender, Player recipient) {
        super(sender, recipient);
    }

    @Override
    public void accepted() {
        Philios.instance.addFriendship(this.getSender().getUniqueId(), this.getRecipient().getUniqueId());
    }
}
