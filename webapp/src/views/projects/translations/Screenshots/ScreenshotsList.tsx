import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import { useTranslationsActions } from '../context/TranslationsContext';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { ScreenshotThumbnail } from './ScreenshotThumbnail';
import { stopAndPrevent } from 'tg.fixtures/eventHandler';

type ScreenshotModel = components['schemas']['ScreenshotModel'];

const MAX_SIZE = 300;
const MIN_SIZE = 100;

const MAX_HEIGHT = 150;

type Props = {
  screenshots: ScreenshotModel[];
  oneBig?: boolean;
  width?: number;
  keyId: number;
  setDetail: (index: number) => void;
  onSrcExpired: () => void;
};

export const ScreenshotsList = ({
  screenshots,
  oneBig,
  width,
  keyId,
  setDetail,
  onSrcExpired,
}: Props) => {
  const size = width && Math.floor(width / 50) * 50;

  const project = useProject();
  let boundedSize: number | undefined = undefined;
  if (size) {
    boundedSize = Math.max(Math.min(MAX_SIZE, size), MIN_SIZE) - 24;
    if (boundedSize < MIN_SIZE) {
      boundedSize = undefined;
    }
  }
  const { updateScreenshots } = useTranslationsActions();

  const oneOnly = Boolean(
    screenshots.length === 1 &&
      boundedSize &&
      oneBig &&
      screenshots.every((sc) => sc.width && sc.height)
  );

  const deleteLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/keys/{keyId}/screenshots/{ids}',
    method: 'delete',
  });

  const handleDelete = (id: number) => {
    deleteLoadable.mutate(
      {
        path: { projectId: project.id, ids: [id], keyId },
      },
      {
        onSuccess() {
          updateScreenshots({
            keyId,
            screenshots(data) {
              return data.filter((sc) => sc.id !== id);
            },
          });
        },
      }
    );
  };

  const calculatedHeight = Math.min(
    Math.max(...screenshots.map((sc) => 100 / (sc.width! / sc.height!))),
    100
  );

  return (
    <>
      {screenshots.map((sc, index) => {
        let width =
          oneOnly && boundedSize ? Math.min(boundedSize, sc.width!) : 100;
        let height =
          oneOnly && boundedSize
            ? width / (sc.width! / sc.height!)
            : calculatedHeight;

        if (height > MAX_HEIGHT && oneOnly) {
          height = MAX_HEIGHT;
          width = height * (sc.width! / sc.height!);
        }

        const isLarge = height > 100 || width > 100;

        const screenshot = {
          id: sc.id,
          src: isLarge ? sc.middleSizedUrl ?? sc.fileUrl : sc.thumbnailUrl,
          width: sc.width,
          height: sc.height,
          highlightedKeyId: keyId,
          keyReferences: sc.keyReferences,
        };

        return (
          <ScreenshotThumbnail
            key={sc.id}
            screenshot={screenshot}
            objectFit={oneOnly ? 'cover' : 'contain'}
            scaleHighlight={(sc.width! / width - 1) * 0.5 + 1}
            onSrcExpired={onSrcExpired}
            onClick={stopAndPrevent(() => {
              setDetail(index);
            })}
            onDelete={() => {
              handleDelete(sc.id);
            }}
            sx={{
              width,
              height,
              scrollSnapAlign: 'center',
            }}
          />
        );
      })}
    </>
  );
};
