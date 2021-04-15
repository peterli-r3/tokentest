package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.commands.MoveTokenCommand;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensUtilities;
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokensHandler;
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow;
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlow;
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlowHandler;
import com.template.contracts.FungEvoTokenContract;
import com.template.states.FungEvoTokenType;
import net.corda.core.contracts.Amount;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.UntrustworthyData;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class TransferFungEvoToken {

    @InitiatingFlow
    @StartableByRPC
    public static class TransferFungEvoTokenInitiator extends FlowLogic<SignedTransaction> {

        private final String tokenTypeLinearId;
        private final Party receiver;
        private final Party observer;
        private final int quantity;

        public TransferFungEvoTokenInitiator(String tokenTypeLinearId, Party receiver, Party observer, int quantity) {
            this.tokenTypeLinearId = tokenTypeLinearId;
            this.receiver = receiver;
            this.observer = observer;
            this.quantity = quantity;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            QueryCriteria inputCriteria = new QueryCriteria.LinearStateQueryCriteria()
                    .withUuid(Arrays.asList(UUID.fromString(tokenTypeLinearId))).withStatus(Vault.StateStatus.UNCONSUMED);

            FungEvoTokenType fungEvoTokenType = getServiceHub().getVaultService().queryBy(FungEvoTokenType.class, inputCriteria)
                    .getStates().get(0).getState().getData();

            Amount<TokenType> amount = new Amount<>(quantity, fungEvoTokenType.toPointer(FungEvoTokenType.class));
            TransactionBuilder txBuilder = new TransactionBuilder(getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0));
            MoveTokensUtilities.addMoveFungibleTokens(txBuilder,getServiceHub(),amount, receiver,getOurIdentity());

            txBuilder.verify(getServiceHub());
            SignedTransaction ptx = getServiceHub().signInitialTransaction((txBuilder));

            FlowSession receiverSession = initiateFlow(receiver);
            FlowSession observerSession = initiateFlow(observer);

            TransactionSignature recSig = receiverSession.sendAndReceive(TransactionSignature.class, ptx).unwrap(it -> it);
            TransactionSignature obsSig = observerSession.sendAndReceive(TransactionSignature.class, ptx).unwrap(it -> it);

            SignedTransaction ftx = ptx.withAdditionalSignatures(Arrays.asList(recSig, obsSig));
            ftx = subFlow(new ObserverAwareFinalityFlow(ftx, Arrays.asList(receiverSession, observerSession)));

            //Add the new token holder to the distribution list
            subFlow(new UpdateDistributionListFlow(ftx));
            return ftx;
        }

    }

    @InitiatedBy(TransferFungEvoTokenInitiator.class)
    public static class TransferFungEvoTokenResponder extends FlowLogic<Void> {

        private FlowSession counterSession;

        public TransferFungEvoTokenResponder(FlowSession counterSession) {
            this.counterSession = counterSession;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {

            SignedTransaction ptx = counterSession.receive(SignedTransaction.class).unwrap(it -> it);
            counterSession.send(getServiceHub().createSignature(ptx));

            //simply use the MoveFungibleTokensHandler as the responding flow
            subFlow(new ObserverAwareFinalityFlowHandler(counterSession));
            return null;
        }
    }
}
// flow start TransferFungEvoTokenInitiator tokenTypeLinearId: 2d3557f0-c71d-4121-aa4f-8a0dcd57ec4d, receiver: InvestorB, observer: TIE, quantity: 20
// https://stackoverflow.com/questions/58866902/what-determines-the-required-keys-to-sign-in-corda
