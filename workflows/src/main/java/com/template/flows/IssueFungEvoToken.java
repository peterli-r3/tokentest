package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import com.r3.corda.lib.tokens.workflows.utilities.FungibleTokenBuilder;
import com.template.states.FungEvoTokenType;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;

import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;

import java.util.*;


@InitiatingFlow
@StartableByRPC
public class IssueFungEvoToken extends FlowLogic<SignedTransaction> {

    private final Party issuedTo;
    private final String uuid;

    public IssueFungEvoToken(String uuid, Party issuedTo) {
        this.uuid = uuid;
        this.issuedTo = issuedTo;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {

        /* Get a reference of own identity */
        Party issuer = getOurIdentity();

        /* Fetch the house state from the vault using the vault query */

        QueryCriteria inputCriteria = new QueryCriteria.LinearStateQueryCriteria()
                .withUuid(Arrays.asList(UUID.fromString(uuid))).withStatus(Vault.StateStatus.UNCONSUMED);

        FungEvoTokenType customTokenState = getServiceHub().getVaultService().queryBy(FungEvoTokenType.class,inputCriteria)
                .getStates().get(0).getState().getData();


        //use token builder
        FungibleToken fungEvoToken = new FungibleTokenBuilder()
                .ofTokenType(customTokenState.toPointer(FungEvoTokenType.class))
                .issuedBy(getOurIdentity())
                .withAmount(100)
                .heldBy(issuedTo)
                .buildFungibleToken();

        return subFlow(new IssueTokens(Arrays.asList(fungEvoToken)));
    }
}

// flow start IssueFungEvoToken uuid: 2d3557f0-c71d-4121-aa4f-8a0dcd57ec4d, issuedTo: InvestorA
// run vaultQuery contractStateType: com.r3.corda.lib.tokens.contracts.states.FungibleToken