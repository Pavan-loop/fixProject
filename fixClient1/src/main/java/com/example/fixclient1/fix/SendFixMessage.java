package com.example.fixclient1.fix;

import com.example.fixclient1.model.ReceivedData;
import com.example.fixclient1.model.TableOrder;
import quickfix.*;

import quickfix.field.*;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.OrderCancelRequest;
import quickfix.fix44.OrderCancelReplaceRequest;

import java.time.LocalDateTime;
import java.util.List;

public class SendFixMessage {

    private static Session getFirstSession(Initiator initiator) {
        if (initiator == null)
            throw new IllegalStateException("FIX Initiator is null. Start the initiator first.");

        List<SessionID> sessions = initiator.getSessions();
        if (sessions == null || sessions.isEmpty())
            throw new IllegalStateException("FIX Initiator not started or no sessions available.");

        return Session.lookupSession(sessions.get(0));
    }

    public static ReceivedData send(Initiator initiator, TableOrder tableOrder) throws SessionNotFound {
        Session session = getFirstSession(initiator);
        char side = tableOrder.getSide().equalsIgnoreCase("BUY") ? Side.BUY : Side.SELL;
        char orderType = tableOrder.getOrderType().equalsIgnoreCase("LIMIT") ? OrdType.LIMIT : OrdType.MARKET;

        String clOrdId = "ORD" + System.currentTimeMillis();

        NewOrderSingle newOrder = new NewOrderSingle(
                new ClOrdID(clOrdId),
                new Side(side),
                new TransactTime(),
                new OrdType(orderType)
        );

        newOrder.set(new Symbol(tableOrder.getSymbol()));
        newOrder.set(new OrderQty(tableOrder.getOrderQuantity()));
        if (orderType == OrdType.LIMIT) {
            newOrder.set(new Price(tableOrder.getOrderPrice()));
        }

        Session.sendToTarget(newOrder, session.getSessionID());

        ReceivedData data = new ReceivedData();
        data.setClOrdId(clOrdId);
        data.setSymbol(tableOrder.getSymbol());
        data.setSide(tableOrder.getSide());
        data.setExecType("Pending");
        data.setPrice(tableOrder.getOrderPrice());
        data.setOriginalQuantity(tableOrder.getOrderQuantity());
        data.setFilledQuantity(0);
        data.setRemainingQuantity(tableOrder.getOrderQuantity());
        data.setCanceledQuantity(0);
        data.setSessionID(session.getSessionID());

        return data;
    }

    public static void cancelOrder(Initiator initiator, ReceivedData receivedData, SessionID activeSessionID) throws SessionNotFound {
        if (activeSessionID == null || !Session.doesSessionExist(activeSessionID)) {
            throw new SessionNotFound();
        }

        String cancelClOrdId = "CL" + System.currentTimeMillis();
        OrderCancelRequest cancelRequest = new OrderCancelRequest(
                new OrigClOrdID(receivedData.getClOrdId()),
                new ClOrdID(cancelClOrdId),
                new Side(receivedData.getSide().equalsIgnoreCase("BUY") ? Side.BUY : Side.SELL),
                new TransactTime(LocalDateTime.now())
        );

        cancelRequest.set(new Symbol(receivedData.getSymbol()));
        Session.sendToTarget(cancelRequest, activeSessionID);

        System.out.println("Sent cancel for order: " + receivedData.getClOrdId());
    }



    public static void replaceOrder(Initiator initiator, String origClOrdId, String newClOrdId,
                                    String symbol, double newPrice, int newQty, char side) throws SessionNotFound {
        Session session = getFirstSession(initiator);

        OrderCancelReplaceRequest replaceRequest = new OrderCancelReplaceRequest();
        replaceRequest.set(new OrigClOrdID(origClOrdId));
        replaceRequest.set(new ClOrdID(newClOrdId));
        replaceRequest.set(new Side(side));
        replaceRequest.set(new Symbol(symbol));
        replaceRequest.set(new TransactTime(LocalDateTime.now()));
        replaceRequest.set(new OrdType(OrdType.LIMIT));
        replaceRequest.set(new Price(newPrice));
        replaceRequest.set(new OrderQty(newQty));
        replaceRequest.set(new TimeInForce(TimeInForce.DAY));

        Session.sendToTarget(replaceRequest, session.getSessionID());
    }
}
