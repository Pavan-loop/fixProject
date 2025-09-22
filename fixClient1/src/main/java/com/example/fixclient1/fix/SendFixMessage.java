package com.example.fixclient1.fix;

import com.example.fixclient1.model.ReceivedData;
import com.example.fixclient1.model.TableOrder;
import quickfix.Initiator;
import quickfix.Session;
import quickfix.SessionNotFound;
import quickfix.field.*;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.OrderCancelRequest;

import java.time.LocalDateTime;

public class SendFixMessage {

    /**
     * Send a new order to the broker and return a ReceivedData object
     * initialized with correct quantities.
     */
    public static ReceivedData send(Initiator initiator, TableOrder tableOrder) throws SessionNotFound {
        char side = tableOrder.getSide().equalsIgnoreCase("BUY") ? Side.BUY : Side.SELL;
        char orderType = tableOrder.getOrderType().equalsIgnoreCase("LIMIT") ? OrdType.LIMIT : OrdType.MARKET;

        String clOrdId = "123" + System.currentTimeMillis();

        NewOrderSingle newOrder = new NewOrderSingle(
                new ClOrdID(clOrdId),
                new Side(side),
                new TransactTime(),
                new OrdType(orderType)
        );

        newOrder.set(new Symbol(tableOrder.getSymbol()));
        newOrder.set(new OrderQty(tableOrder.getOrderQuantity()));
        newOrder.set(new Price(tableOrder.getOrderPrice()));

        Session.sendToTarget(newOrder, initiator.getSessions().get(0));


        ReceivedData data = new ReceivedData();
        data.setClOrdId(clOrdId);
        data.setExecId(data.getExecId());
        data.setSymbol(tableOrder.getSymbol());
        data.setSide(tableOrder.getSide());
        data.setExecType("New");
        data.setPrice(tableOrder.getOrderPrice());
        data.setOriginalQuantity(tableOrder.getOrderQuantity());
        data.setFilledQuantity(0);
        data.setRemainingQuantity(tableOrder.getOrderQuantity());
        data.setCanceledQuantity(0);
        data.setSessionID(initiator.getSessions().get(0));

        return data;
    }

    /**
     * Send a cancel request for an existing order.
     */
    public static void cancelOrder(Initiator initiator, ReceivedData receivedData) throws SessionNotFound {
        String cancelClOrdId = "CL" + System.currentTimeMillis();

        OrderCancelRequest cancelRequest = new OrderCancelRequest(
                new OrigClOrdID(receivedData.getClOrdId()),
                new ClOrdID(cancelClOrdId),
                new Side(receivedData.getSide().equalsIgnoreCase("BUY") ? Side.BUY : Side.SELL),
                new TransactTime(LocalDateTime.now())
        );

        cancelRequest.set(new Symbol(receivedData.getSymbol()));

        Session.sendToTarget(cancelRequest, receivedData.getSessionID());
    }
}
