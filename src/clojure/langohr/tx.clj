;; This source code is dual-licensed under the Apache License, version
;; 2.0, and the Eclipse Public License, version 1.0.
;;
;; The APL v2.0:
;;
;; ----------------------------------------------------------------------------------
;; Copyright (c) 2011-2024 Michael S. Klishin, Alex Petrov, and the ClojureWerkz Team
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.
;; ----------------------------------------------------------------------------------
;;
;; The EPL v1.0:
;;
;; ----------------------------------------------------------------------------------
;; Copyright (c) 2011-2024 Michael S. Klishin, Alex Petrov, and the ClojureWerkz Team.
;; All rights reserved.
;;
;; This program and the accompanying materials are made available under the terms of
;; the Eclipse Public License Version 1.0,
;; which accompanies this distribution and is available at
;; http://www.eclipse.org/legal/epl-v10.html.
;; ----------------------------------------------------------------------------------

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
