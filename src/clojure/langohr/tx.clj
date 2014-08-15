;; Copyright (c) 2011-2014 Michael S. Klishin
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns langohr.tx
  "Transaction operations"
  (:import com.rabbitmq.client.Channel
           [com.novemberain.langohr.tx SelectOk CommitOk RollbackOk]))


;;
;; API
;;

(defn ^com.novemberain.langohr.tx.SelectOk select
  "Activates transactions on given channel. Please note that transactions only
   cover publishing and acknowledgements, not delivery to consumers."
  [^Channel channel]
  (SelectOk. (.txSelect channel)))


(defn ^com.novemberain.langohr.tx.CommitOk commit
  "Commits current transaction"
  [^Channel channel]
  (CommitOk. (.txCommit channel)))


(defn ^com.novemberain.langohr.tx.CommitOk rollback
  "Rolls current transaction back"
  [^Channel channel]
  (RollbackOk. (.txRollback channel)))
