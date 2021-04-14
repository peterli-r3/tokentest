

flow start CreateFungEvoTokenType issuer: InvestorA, msg: hello

flow start IssueFungEvoToken uuid: 5677215e-ad94-4a59-ba6a-d6acfc936da1, issuedTo: InvestorA

flow start TransferFungEvoTokenInitiator tokenTypeLinearId: 5677215e-ad94-4a59-ba6a-d6acfc936da1, receiver: InvestorB, observer: TIE, quantity: 20
 
run vaultQuery contractStateType: com.r3.corda.lib.tokens.contracts.states.FungibleToken