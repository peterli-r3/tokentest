package com.template.states;

import com.template.contracts.NormalContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@BelongsToContract(NormalContract.class)
public class NormalState implements ContractState {

    private Party owner;
    private String msg;
    private List<AbstractParty> participants;

    public NormalState(Party owner, String msg) {
        this.owner = owner;
        this.msg = msg;
        this.participants = new ArrayList<>();
        this.participants.add(this.owner);
    }

    public Party getOwner() {
        return owner;
    }

    public String getMsg() {
        return msg;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return participants;
    }
}
