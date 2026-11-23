-- One row per message the customer received. Written by the Kafka notification consumer.
-- No FK to orders on purpose: in a real system the notification service has its own database
-- and cannot reference another service's tables. It only knows the order id from the event.
CREATE TABLE notifications (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id   BIGINT       NOT NULL,
    channel    VARCHAR(20)  NOT NULL,   -- SMS, PUSH
    message    VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_order_id ON notifications (order_id);
