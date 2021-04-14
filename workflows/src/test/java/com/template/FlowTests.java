package com.template;

import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilities;
import com.template.flows.CreateFungEvoTokenType;
import com.template.flows.IssueFungEvoToken;
import com.template.flows.TransferFungEvoToken;
import com.template.states.FungEvoTokenType;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.NetworkParameters;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.security.SignatureException;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
public class FlowTests {

    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;
    private StartedMockNode c;

    private NetworkParameters testNetworkParameters =
            new NetworkParameters(4, Arrays.asList(), 10485760, (10485760 * 5), Instant.now(),1, new LinkedHashMap<>());

    @Before
    public void setup() {

        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("com.template.contracts"),
                TestCordapp.findCordapp("com.template.flows"),
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"),
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows")))
                .withNetworkParameters(testNetworkParameters)
                .withNotarySpecs(Arrays.asList(new MockNetworkNotarySpec(new CordaX500Name("Notary", "London", "GB")))));
        a = network.createPartyNode(new CordaX500Name("InvestorA", "TestLand", "US"));
        b = network.createPartyNode(new CordaX500Name("InvestorB", "TestCity", "US"));
        c = network.createPartyNode(new CordaX500Name("TIE", "TestVillage", "US"));
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void returnsFullySignedTxFromAllParties() throws ExecutionException, InterruptedException, SignatureException {
        Party investorA = a.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        Party investorB = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        Party tie =  c.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();

        //Create TokenType at A
        CreateFungEvoTokenType myFlow = new CreateFungEvoTokenType(investorA,"hello");
        Future<String> future = a.startFlow(myFlow);
        network.runNetwork();

        String resString = future.get();
        System.out.println(resString);

        int subString = resString.indexOf("with Id: ");
        String uuid = resString.substring(subString+9);
        System.out.println("-"+uuid+"-");



        //Issue 100 to investorA
        IssueFungEvoToken issueFlow = new IssueFungEvoToken(uuid,investorA);
        Future<SignedTransaction> futureIssue = a.startFlow(issueFlow);
        network.runNetwork();
        SignedTransaction ptx2 = futureIssue.get();


        QueryCriteria inputCriteria = new QueryCriteria.LinearStateQueryCriteria()
                .withUuid(Arrays.asList(UUID.fromString(uuid))).withStatus(Vault.StateStatus.UNCONSUMED);

        FungEvoTokenType customTokenState = a.getServices().getVaultService().queryBy(FungEvoTokenType.class,inputCriteria)
                .getStates().get(0).getState().getData();

        TokenPointer<FungEvoTokenType> tokenPointer =  customTokenState.toPointer(FungEvoTokenType.class);
        //query balance or each different Token
        Amount<TokenType> amount = QueryUtilities.tokenBalance(a.getServices().getVaultService(), tokenPointer);
        System.out.println("\nA currently have "+ amount.getQuantity());

        //Transfer 20 tokens from A to B, with tie signature on transaction.
        TransferFungEvoToken.TransferFungEvoTokenInitiator transferFlow = new TransferFungEvoToken.TransferFungEvoTokenInitiator(uuid,investorB,tie,20);
        Future<SignedTransaction> futureTransfer = a.startFlow(transferFlow);
        network.runNetwork();
        SignedTransaction ptx3 = futureTransfer.get();


        //query balance or each different Token
        Amount<TokenType> amountB = QueryUtilities.tokenBalance(b.getServices().getVaultService(), tokenPointer);
        System.out.println("\nB currently have "+ amountB.getQuantity());



        System.out.println("------------------------");
        System.out.println(ptx3.getRequiredSigningKeys().size());
        System.out.println(ptx3.getRequiredSigningKeys());
        System.out.println("Signers------------------------");
        System.out.println(ptx3.getTx().getCommands().get(0).getSigners());

        System.out.println("Signers------------------------");
        System.out.println(ptx3.getTx().getCommands().get(0).getSigners());
    }


}
