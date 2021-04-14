package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens;
import com.template.states.FungEvoTokenType;
import net.corda.core.contracts.TransactionState;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;

@InitiatingFlow
@StartableByRPC
public class CreateFungEvoTokenType extends FlowLogic<String> {

    private Party issuer;
    private String msg;

    public CreateFungEvoTokenType(Party issuer, String msg) {
        this.issuer = issuer;
        this.msg = msg;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {

        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        final FungEvoTokenType fungEvoToken = new FungEvoTokenType(this.msg,this.getOurIdentity());

        TransactionState transactionState = new TransactionState(fungEvoToken, notary);
        SignedTransaction stx = subFlow(new CreateEvolvableTokens(transactionState));
        return "TokenType Created with Id: " + fungEvoToken.getLinearId().toString();
    }
}

// flow start CreateFungEvoTokenType issuer: InvestorA, msg: hello
// run vaultQuery contractStateType: com.template.states.FungEvoTokenType