# Build curl
include $(CURRENT_DIR)/curl/Android.mk


include $(CLEAR_VARS)


# SIGC++ Library built as static library
LOCAL_MODULE := sigc
LOCAL_PATH = $(CURRENT_DIR)
LOCAL_CPP_EXTENSION := .cc



LOCAL_SRC_FILES :=    sigc++/signal.cc       sigc++/signal_base.cc  sigc++/trackable.cc 
LOCAL_SRC_FILES +=    sigc++/functors/slot_base.cc  sigc++/adaptors/lambda/lambda.cc 
LOCAL_SRC_FILES += sigc++/connection.cc sigc++/functors/slot.cc


LOCAL_C_INCLUDES := sigc++

include $(BUILD_STATIC_LIBRARY)

# Torrent library

include $(CLEAR_VARS)


LOCAL_PATH := $(CURRENT_DIR)/libtorrent/src
LOCAL_C_INCLUDES :=  $(CURRENT_DIR)/libtorrent $(CURRENT_DIR)  $(CURRENT_DIR)/libtorrent/src $(CURRENT_DIR)/libtorrent/src/torrent
LOCAL_CPP_EXTENSION := .cc
LOCAL_MODULE = rtorrent

LOCAL_SRC_FILES = globals.cc resource_manager.cc manager.cc

#torrent subdirs
LOCAL_SRC_FILES+= torrent/data/block.cc torrent/data/block_list.cc torrent/data/chunk_utils.cc torrent/data/file.cc torrent/data/file_list.cc torrent/data/file_list_iterator.cc torrent/data/file_manager.cc torrent/data/file_utils.cc torrent/data/transfer_list.cc torrent/peer/client_info.cc torrent/peer/client_list.cc torrent/peer/connection_list.cc torrent/peer/peer.cc torrent/peer/peer_info.cc torrent/peer/peer_list.cc



#data
LOCAL_SRC_FILES+= data/chunk.cc data/chunk_list.cc data/chunk_part.cc data/hash_chunk.cc data/hash_queue.cc data/hash_queue_node.cc data/hash_torrent.cc data/memory_chunk.cc data/socket_file.cc

# dht 
LOCAL_SRC_FILES+= dht/dht_bucket.cc dht/dht_node.cc dht/dht_router.cc dht/dht_server.cc dht/dht_tracker.cc dht/dht_transaction.cc

#download 
LOCAL_SRC_FILES+= download/available_list.cc download/choke_manager.cc download/chunk_selector.cc download/chunk_statistics.cc download/delegator.cc download/download_constructor.cc download/download_main.cc download/download_manager.cc download/download_wrapper.cc

# net
LOCAL_SRC_FILES+=net/address_list.cc net/listen.cc net/socket_base.cc net/socket_datagram.cc net/socket_fd.cc net/socket_set.cc net/socket_stream.cc net/throttle_internal.cc net/throttle_list.cc

#protocol
LOCAL_SRC_FILES+=protocol/extensions.cc protocol/handshake.cc protocol/handshake_encryption.cc protocol/handshake_manager.cc protocol/initial_seed.cc protocol/peer_connection_base.cc protocol/peer_connection_leech.cc protocol/peer_connection_metadata.cc protocol/peer_factory.cc protocol/request_list.cc

# torrent
LOCAL_SRC_FILES+= torrent/bitfield.cc torrent/chunk_manager.cc torrent/connection_manager.cc torrent/dht_manager.cc torrent/download.cc torrent/error.cc torrent/exceptions.cc torrent/hash_string.cc torrent/http.cc torrent/object.cc torrent/object_static_map.cc torrent/object_stream.cc torrent/path.cc torrent/poll_epoll.cc torrent/poll_kqueue.cc torrent/poll_select.cc torrent/rate.cc torrent/resume.cc torrent/thread_base.cc torrent/throttle.cc torrent/torrent.cc torrent/tracker.cc torrent/tracker_list.cc


#tracker
LOCAL_SRC_FILES+= tracker/tracker_dht.cc tracker/tracker_http.cc tracker/tracker_manager.cc tracker/tracker_udp.cc

#utils
LOCAL_SRC_FILES+= utils/diffie_hellman.cc  utils/sha_fast.cc


LOCAL_STATIC_LIBRARIES := sigc

include $(BUILD_STATIC_LIBRARY)
