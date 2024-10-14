import * as React from "react";
import Box from "@mui/material/Box";
import Modal from "@mui/material/Modal";
import { useTranslate } from '@tolgee/react';

const style = {
  position: "absolute",
  top: "50%",
  left: "50%",
  transform: "translate(-50%, -50%)",
  bgcolor: "background.paper",
  boxShadow: 24,
  paddingInline: "24px",
  paddingBlock: "20px",
  borderRadius: '12px'
};

export default function VideoModal({
  name,
  modalVisible,
  setModalVisible,
}) {
  const { t } = useTranslate();
  const handleClose = () => setModalVisible(false);

  return (
    <Modal
      open={modalVisible}
      onClose={handleClose}
      aria-labelledby="modal-modal-title"
      aria-describedby="modal-modal-description"
    >
      <Box sx={style}>
        <h1 className="text-lg text-center font-semibold border-b pb-2">
         <span className="capitalize">{t(name,name)}</span> {t('tutorial','tutorial')}
        </h1>
        <figure className="w-[320px] max-w-[575px] sm:w-auto sm:min-w-[400px] aspect-video">
          <video className="w-full h-full" controls preload="metadata">
            <source src={`./videos/${name}.mp4`} type="video/mp4" />
          </video>
        </figure>
      </Box>
    </Modal>
  );
}
