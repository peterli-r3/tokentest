package com.template.states;

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType;
import com.template.contracts.FungEvoTokenContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
@BelongsToContract(FungEvoTokenContract.class)
public class FungEvoTokenType extends EvolvableTokenType {

    private String someData;
    private List<Party> maintainers;
    private Party issuer;
    private int fractionDigits = 0;
    private UniqueIdentifier linearId;


    @ConstructorForDeserialization
    public FungEvoTokenType(String someData, List<Party> maintainers, Party issuer, int fractionDigits, UniqueIdentifier linearId) {
        this.someData = someData;
        this.maintainers = maintainers;
        this.issuer = issuer;
        this.fractionDigits = fractionDigits;
        this.linearId = linearId;
    }

    public FungEvoTokenType(String someData, Party issuer) {
        this.someData = someData;
        this.issuer = issuer;


        this.maintainers = new ArrayList<>();
        this.maintainers.add(this.issuer);
        this.linearId = new UniqueIdentifier();
    }

    @Override
    public int getFractionDigits() {
        return this.fractionDigits;
    }

    @NotNull
    @Override
    public List<Party> getMaintainers() {
        return this.maintainers;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return this.linearId;
    }
}
