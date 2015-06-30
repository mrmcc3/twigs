(ns twigs.protocols)

(defprotocol IPub
  (-once [_ sub err])
  (-on! [_ topic sub err])
  (-off! [_] [_ topic] [_ topic sub]))

(defprotocol IRef
  (-raw-ref [_]))
