package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;

import com.template.contracts.NormalContract;
import com.template.states.NormalState;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class NormalTransaction {

    @InitiatingFlow
    @StartableByRPC
    public static class NormalTransactionInitiator extends FlowLogic<SignedTransaction> {


        //private variables
        private Party sender ;
        private Party receiver;
        private Party observer;

        //public constructor
        public NormalTransactionInitiator(Party sendTo, Party observer){
            this.receiver = sendTo;
            this.observer = observer;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            //Hello World message
            String msg = "Hello-World";
            this.sender = getOurIdentity();

            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            //Compose the State that carries the Hello World message
            final NormalState output = new NormalState(receiver,msg);

            final TransactionBuilder builder = new TransactionBuilder(notary)
                    .addOutputState(output)
                    .addCommand(new NormalContract.Commands.Send(),
                            Arrays.asList(this.sender.getOwningKey(),this.receiver.getOwningKey(),this.observer.getOwningKey()));
            //At this step, we sre requesdting additional signatures.

            builder.verify(getServiceHub());
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

            FlowSession receiverSession = initiateFlow(receiver);
            FlowSession observerSession = initiateFlow(observer);
            SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, Arrays.asList(receiverSession, observerSession)));

            // Step 7. Assuming no exceptions, we can now finalise the transaction
            return subFlow(new FinalityFlow(stx, Arrays.asList(receiverSession, observerSession)));
        }
    }
    @InitiatedBy(NormalTransactionInitiator.class)
    public static class Responder extends FlowLogic<Void> {

        //private variable
        private FlowSession counterpartySession;

        //Constructor
        public Responder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            SignedTransaction signedTransaction = subFlow(new SignTransactionFlow(counterpartySession) {
                @Suspendable
                @Override
                protected void checkTransaction(SignedTransaction stx) throws FlowException {

                }
            });
            //Stored the transaction into data base.
            subFlow(new ReceiveFinalityFlow(counterpartySession, signedTransaction.getId()));
            return null;
        }
    }


}
